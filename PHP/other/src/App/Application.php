<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace App;

use Core\Di\Container;

/**
 * Class Application
 */
class Application
{
    /**
     * @var Container
     */
    private $container;

    /**
     * @var PrinterInterface
     */
    private $printer;

    /**
     * Application constructor.
     *
     * @param Container $container
     */
    public function __construct(Container $container, PrinterInterface $printer)
    {
        $this->container = $container;
        $this->printer = $printer;
    }

    /**
     * @return void
     */
    public function run()
    {
        $this->printer->printText();
    }
}
